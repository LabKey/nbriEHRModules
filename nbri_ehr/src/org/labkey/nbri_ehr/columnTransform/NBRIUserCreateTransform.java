/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.nbri_ehr.columnTransform;

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.di.columnTransform.ColumnTransform;
import org.labkey.api.di.columnTransform.ColumnTransformException;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.Group;
import org.labkey.api.security.InvalidGroupMembershipException;
import org.labkey.api.security.MemberType;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.ValidEmail;
import org.labkey.nbri_ehr.query.NBRI_EHRUserSchema;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NBRIUserCreateTransform extends ColumnTransform
{
    private transient final Map<String, Integer> _sourceUserNameMap = new HashMap<>();

    /**
     * Search for the user by the first|last name (ex. john|doe)
     * Return null if user not found.
     */
    private Integer getUserByFirstLastName(String name)
    {
        Collection<User> users = UserManager.getUsers(true);
        for (User user : users)
        {
            if (user.getFirstName() != null && user.getLastName() != null && user.getFirstName().concat("|").concat(user.getLastName()).equalsIgnoreCase(name))
            {
                return user.getUserId();
            }
        }

        return null;
    }

    private User createUser(String name)
    {
        String[] nameParts = name.split("\\|");

        if (nameParts.length < 2)
            return null;

        UserSchema us = QueryService.get().getUserSchema(getContainerUser().getUser(), getContainerUser().getContainer(), NBRI_EHRUserSchema.SCHEMA_NAME);

        SQLFragment sql = new SQLFragment("SELECT email, displayName, officePhone, homePhone, cellPhone, positionName FROM ").append(Objects.requireNonNull(us.getTable("Staff")), "st");
        sql.append(" WHERE (lower(firstName) = ? AND lower(lastName) = ?) LIMIT 1");
        sql.add(nameParts[0]);
        sql.add(nameParts[1]);

        User newUser = new User();
        new SqlSelector(us.getDbSchema(), sql).forEachMap(row -> {
            String displayName;
            if (row.get("displayName") != null)
            {
                displayName = (String)row.get("displayName");
            }
            else
            {
                String EMAIL_PATTERN = "[^a-zA-Z0-9!#$%&@'*+{-}/=?^_`{|}~.]+";
                displayName = (nameParts[0] + nameParts[1]).replaceAll(EMAIL_PATTERN, "");
            }

            newUser.setFirstName(nameParts[0]);
            newUser.setLastName(nameParts[1]);
            if(row.get("email") != null)
            {
                newUser.setEmail((String) row.get("email"));
            }
            else
            {
                newUser.setEmail(displayName + "@louisiana.edu");
            }

            if(row.get("officePhone") != null)
            {
                newUser.setPhone((String) row.get("officePhone"));
            }
            else
            {
                newUser.setPhone((String) row.get("homePhone"));
            }

            newUser.setDisplayName(displayName);
            newUser.setMobile((String) row.get("cellPhone"));
            newUser.setDescription((String) row.get("positionName"));
        });

        return newUser;
    }

    /**
     * Performs user lookup or creation of new user. Returns the user id to populate the
     * column with.
     */
    @Override
    protected Object doTransform(Object inputValue)
    {
        if(null == inputValue)
            return null;

        // comparing with and making this an email name so case insensitive
        String input = inputValue.toString().toLowerCase();

        // See if user has already been found anywhere
        Integer userId = _sourceUserNameMap.get(input);

        // Attempt to look up user in user table
        if(null == userId)
        {
            userId = getUserByFirstLastName(input);
        }

        // First time encountering user, create deactivated account
        if (null == userId)
        {
            try
            {
                User user = createUser(input);
                if (user != null)
                {
                    User savedUser;
                    // Check if user created already for this email otherwise create new one
                    if (UserManager.userExists(new ValidEmail(user.getEmail())))
                    {
                        savedUser = UserManager.getUser(new ValidEmail(user.getEmail()));
                    }
                    else
                    {
                        savedUser = SecurityManager.addUser(new ValidEmail(user.getEmail()), getContainerUser().getUser(), false).getUser();
                    }
                    user.setCreated(savedUser.getCreated());
                    user.setCreatedBy(savedUser.getCreatedBy());
                    user.setUserId(savedUser.getUserId());
                    user.setEntityId(savedUser.getEntityId());
                    user.setActive(savedUser.isActive());

                    UserManager.updateUser(getContainerUser().getUser(), user);
                    UserManager.setUserActive(getContainerUser().getUser(), user, false);

                    userId = user.getUserId();
                    String userGrp = "NBRI ETL USERS";
                    Integer groupId = SecurityManager.getGroupId(getContainerUser().getContainer().getProject(), userGrp, false);
                    Group group;

                    //create group
                    if (null == groupId)
                    {
                        //createGroup() throws an error if security groups are not associated with a project,
                        // so get the project instead of just getContainerUser().getContainer(), which could be a folder
                        group = SecurityManager.createGroup(getContainerUser().getContainer().getProject(), userGrp, getContainerUser().getUser());
                    }
                    else
                    {
                        group = SecurityManager.getGroup(groupId);
                    }

                    //add user to the group
                    if (SecurityManager.getGroupMembers(group, MemberType.ALL_GROUPS_AND_USERS).stream().noneMatch(p -> p.getName().equalsIgnoreCase(user.getName())))
                        SecurityManager.addMember(group, SecurityManager.getPrincipal(userId));

                }
            }
            catch (ValidEmail.InvalidEmailException e)
            {
                throw new ColumnTransformException("Unable to create email address for user: " + input, e);
            }
            catch (SecurityManager.UserManagementException e)
            {
                throw new ColumnTransformException("Unable to add user:" + input, e);
            }
            catch (InvalidGroupMembershipException e)
            {
                throw new ColumnTransformException("Unable to add user to group: " + input, e);
            }
        }

        _sourceUserNameMap.put(input, userId);

        return userId;
    }
}
